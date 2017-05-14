#!/usr/bin/env groovy

pipeline {
    agent {
        label 'java8'
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'build/test-results/**/*.xml'
        }
    }

    environment {
        GRADLE_OPTIONS = "--no-daemon --rerun-tasks -PBUILD_NUMBER=${env.BUILD_NUMBER} -PBRANCH='${env.BRANCH_NAME}'"
    }

    stages {
        stage('Checkout') {
            steps {
                sh "rm -Rv build || true"
            }
        }

        stage('Build & Test') {
            steps {
                sh "./gradlew ${env.GRADLE_OPTIONS} clean build test"
            }
        }

        stage('Coverage') {
            steps {
                sh "./gradlew ${env.GRADLE_OPTIONS} jacocoTestReport"

                step([$class: 'JacocoPublisher'])
            }
        }
    }
}
