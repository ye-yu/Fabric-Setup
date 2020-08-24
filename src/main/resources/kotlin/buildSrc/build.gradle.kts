/**
* MIT License
* Copyright (c) 2020 Ricky12Awesome
*/

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin.sourceSets["main"].kotlin.srcDir("./src")
