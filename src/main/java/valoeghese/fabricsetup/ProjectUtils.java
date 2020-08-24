package valoeghese.fabricsetup;

import com.google.common.base.CaseFormat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProjectUtils {
	public static final Pattern INVALID_MAIN_CLASS_NAME = Pattern.compile("(^[0-9-]|[^a-zA-Z0-9-])");
	public static final Pattern INVALID_PACKAGE_NAME = Pattern.compile("(^[0-9.]|[^a-zA-Z0-9._])");

	public static final String GRADLE_DIR = "gradle/wrapper";
	public static final String GRADLE_WRAPPER_JAR = "gradle-wrapper.jar";
	public static final String GRADLE_WRAPPER_TEMPLATE_PROPERTIES = "gradle-wrapper-template.properties";
	public static final String GRADLE_WRAPPER_PROPERTIES = "gradle-wrapper.properties";
	public static final String GRADLE_VERSION = "%GRADLEVERSION%";

	public static final String BUILD_GRADLE = "build.gradle.kts";
	public static final String SETTINGS_GRADLE = "settings.gradle.kts";
	public static void createKotlinProject(String group, final String workspaceName, String gradleVersion) throws IOException {
		validateWorkspaceName(workspaceName);
		validateGroupName(group);

		final File projectDirectory = new File(workspaceName);
		final File kotlinSourceDirectory = new File(workspaceName, "src/main/kotlin");

		createDirectoryWithAssertion(projectDirectory);
		importGradleWrapper(gradleVersion, projectDirectory);
		createGradleBuildSettings(projectDirectory, workspaceName, group);
		createDirectoriesWithAssertion(kotlinSourceDirectory);

		final String mainClassName = parseMainClassName(workspaceName);
		createPackage(kotlinSourceDirectory, group, mainClassName);
	}

	private static void validateGroupName(String packageName) {
		if (INVALID_PACKAGE_NAME.matcher(packageName).find()) {
			throw new IllegalArgumentException("Project name must only contains alphanumerics and hyphens.");
		}

		if (packageName.charAt(0) >= '0' && packageName.charAt(0) <= '9' || packageName.charAt(0) == '.') {
			throw new IllegalArgumentException("Project name cannot start with a non-alphabet.");
		}
	}

	private static void validateWorkspaceName(String workspaceName) {
		if (INVALID_MAIN_CLASS_NAME.matcher(workspaceName).find()) {
			throw new IllegalArgumentException("Project name must only contains alphanumerics and hyphens.");
		}

		if (workspaceName.charAt(0) >= '0' && workspaceName.charAt(0) <= '9' || workspaceName.charAt(0) == '-') {
			throw new IllegalArgumentException("Project name cannot start with a non-alphabet.");
		}
	}

	private static String parseMainClassName(final String workspaceName) {
		if (workspaceName.contains("-")) {
			return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, workspaceName.toLowerCase());
		} else {
			return StringUtils.capitalize(workspaceName);
		}
	}

	private static void createPackage(final File directory, final String packageName, final String mainClass) {
		final File packageDir = new File(directory, packageName.replace('.', '/'));
		createDirectoriesWithAssertion(packageDir);

		final File mainClassFile = new File(packageDir, mainClass + ".kt");
		System.out.println("Creating " + mainClassFile);
	}

	public static void importGradleWrapper(final String version, final File directory) throws IOException {
		System.out.printf("Importing gradle version %s to directory %s%n", version, directory.toString());
		final File gradleWrapperDirectory = new File(directory, GRADLE_DIR);
		createDirectoriesWithAssertion(gradleWrapperDirectory);
		FileUtils.copyInputStreamToFile(ResourceManager.load(GRADLE_DIR + "/" + GRADLE_WRAPPER_JAR), new File(gradleWrapperDirectory, GRADLE_WRAPPER_JAR));
		final String gradleWrapperProperties = ResourceManager.loadAsString(GRADLE_DIR + "/" + GRADLE_WRAPPER_TEMPLATE_PROPERTIES).replace(GRADLE_VERSION, version);
		FileWriter fileWriter = new FileWriter(new File(gradleWrapperDirectory, GRADLE_WRAPPER_PROPERTIES));
		fileWriter.write(gradleWrapperProperties);
		fileWriter.close();
	}

	public static void createGradleBuildSettings(final File directory, String workspaceName, String group) throws IOException {
		FileUtils.copyInputStreamToFile(ResourceManager.load("kotlin/" + BUILD_GRADLE), new File(directory, BUILD_GRADLE));
		FileUtils.copyInputStreamToFile(ResourceManager.load("kotlin/" + SETTINGS_GRADLE), new File(directory, SETTINGS_GRADLE));

		final File buildSrc = new File(directory, "buildSrc");
		createDirectoryWithAssertion(buildSrc);
		FileUtils.copyInputStreamToFile(ResourceManager.load("kotlin/buildSrc/" + BUILD_GRADLE), new File(buildSrc, BUILD_GRADLE));

		final File src = new File(buildSrc, "src");
		createDirectoryWithAssertion(src);

		FileUtils.copyInputStreamToFile(ResourceManager.load("kotlin/buildSrc/src/Dependencies.kt"), new File(src, "Dependencies.kts"));
		final String infoKts = ResourceManager.loadAsString("kotlin/buildSrc/src/Info.kt");
		final FileWriter fileWriter = new FileWriter(new File(src, "Info.kt"));
		fileWriter.write(infoKts.replace("%MODID%", workspaceName)
				.replace("%MODNAME%", toName(workspaceName))
				.replace("%GROUP%", group));
		fileWriter.close();
	}

	private static String toName(String workspaceName) {
		if (!workspaceName.contains("-")) return workspaceName;
		return Arrays.stream(workspaceName.split("-")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
	}

	public static void createDirectoryWithAssertion(File file) {
		if (!file.mkdir()) throw new IllegalStateException("Cannot create directory of " + file);
	}

	public static void createDirectoriesWithAssertion(File file) {
		if (!file.mkdirs()) throw new IllegalStateException("Cannot create directories of " + file);
	}

	public static void assertWithFailOf(Runnable fn, Class<? extends Throwable> expectedThrown, String runId) {
		try {
			fn.run();
			if (expectedThrown != null) System.err.println(runId + ": Run was successful, but expected error of " + expectedThrown.getSimpleName());
			else System.out.println(runId + ": Successful");
		} catch (Exception e) {
			if (expectedThrown != null && expectedThrown.isInstance(e)) {
				System.out.println(runId + ": Successful with " + expectedThrown.getSimpleName() + " caught.");
			} else {
				System.err.println(runId + ": Run failed, but expected error of " + (expectedThrown == null ? "null" : expectedThrown.getSimpleName()) + ", got " + e.getClass().getSimpleName() + " instead.");
			}
		}
	}

	public static <T> void assertEquals(Supplier<T> fn, T expected) {
		final T actual = fn.get();
		if (actual.equals(expected)) {
			System.out.println("Assertion of equals " + expected + " is successful.");
		} else {
			System.err.println("Expected " + expected + " but got " + actual + "instead!");
		}
	}

	public static void tests() {
		assertWithFailOf(() -> validateGroupName("com.abss.erw"), null, "com.abss.erw");
		assertWithFailOf(() -> validateGroupName("com.abss_erw"), null, "com.abss_erw");
		assertWithFailOf(() -> validateGroupName("com"), null, "com");
		assertWithFailOf(() -> validateGroupName("_com"), null, "_com");
		assertWithFailOf(() -> validateGroupName("com-abs"), IllegalArgumentException.class, "com-abs");
		assertWithFailOf(() -> validateGroupName("1com"), IllegalArgumentException.class, "1com");
		assertWithFailOf(() -> validateGroupName(".com"), IllegalArgumentException.class, ".com");
		assertWithFailOf(() -> validateGroupName("-com"), IllegalArgumentException.class, "-com");

		assertWithFailOf(() -> validateWorkspaceName("modid-abc"), null, "modid-abc");
		assertWithFailOf(() -> validateWorkspaceName("modid"), null, "modid");
		assertWithFailOf(() -> validateWorkspaceName("1modid"), IllegalArgumentException.class, "1modid");
		assertWithFailOf(() -> validateWorkspaceName(".modid"), IllegalArgumentException.class, ".modid");
		assertWithFailOf(() -> validateWorkspaceName("modid."), IllegalArgumentException.class, "modid.");
		assertWithFailOf(() -> validateWorkspaceName("-modid"), IllegalArgumentException.class, "-modid");

		assertEquals(() -> parseMainClassName("abc-def"), "AbcDef");
		assertEquals(() -> parseMainClassName("abcdef"), "Abcdef");
	}

	public static void main(String... args) throws IOException {
		tests();

		Main.refreshPropertiesFile();

		final File testProject = new File("test-project");
		if (testProject.exists()) FileUtils.deleteDirectory(testProject);
		createKotlinProject("test.create", "test-project", "6.5.1");
	}
}
