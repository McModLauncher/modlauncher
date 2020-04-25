@Library('forge-shared-library')_

pipeline {
    agent {
        docker {
            image 'gradle:jdk8'
            args '-v modlaunchergc:/home/gradle/.gradle/'
        }
    }
    environment {
        GRADLE_ARGS = '-Dorg.gradle.daemon.idletimeout=5000'
    }

    stages {
        stage('buildandtest') {
            steps {
                sh './gradlew ${GRADLE_ARGS} --refresh-dependencies --continue build test'
                script {
                    env.MYVERSION = sh(returnStdout: true, script: './gradlew properties -q | grep "version:" | awk \'{print $2}\'').trim()
                }
            }
            post {
                success {
                    writeChangelog(currentBuild, 'build/changelog.txt')
                    archiveArtifacts artifacts: 'build/changelog.txt', fingerprint: false
                }
            }
        }
        stage('publish') {
            when {
                not {
                    changeRequest()
                }
            }
            environment {
                FORGE_MAVEN = credentials('forge-maven-cpw-user')
            }
            steps {
                sh './gradlew ${GRADLE_ARGS} publish -PforgeMavenUser=${FORGE_MAVEN_USR} -PforgeMavenPassword=${FORGE_MAVEN_PSW}'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'build/libs/**/*.jar', fingerprint: true
            junit 'build/test-results/*/*.xml'
            jacoco sourcePattern: '**/src/*/java'
        }
    }
}