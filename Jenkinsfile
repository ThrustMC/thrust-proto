// Requires Docker Pipeline plugin and Multibranch Pipeline with tag discovery.
// Credentials: nexus-credentials.

pipeline {
    agent any

    options {
        timeout(time: 20, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git fetch --tags --force'
                sh 'git fetch origin main:refs/remotes/origin/main || true'
            }
        }

        stage('Lint') {
            agent {
                docker {
                    image 'bufbuild/buf:1.68.1'
                    reuseNode true
                }
            }
            steps {
                sh 'buf lint'
            }
        }

        stage('Format Check') {
            agent {
                docker {
                    image 'bufbuild/buf:1.68.1'
                    reuseNode true
                }
            }
            steps {
                sh '''
                    diff="$(buf format --diff)"
                    if [ -n "$diff" ]; then
                        echo "$diff"
                        exit 1
                    fi
                '''
            }
        }

        stage('Breaking - PR') {
            when {
                allOf {
                    not { branch 'main' }
                    not { buildingTag() }
                }
            }
            agent {
                docker {
                    image 'bufbuild/buf:1.68.1'
                    reuseNode true
                }
            }
            steps {
                sh 'buf breaking --against ".git#branch=main,ref=refs/remotes/origin/main"'
            }
        }

        stage('Breaking - Release') {
            when { buildingTag() }
            agent {
                docker {
                    image 'bufbuild/buf:1.68.1'
                    reuseNode true
                }
            }
            steps {
                sh '''
                    prev="$(git describe --tags --match 'v*' --abbrev=0 "${TAG_NAME}^" 2>/dev/null || true)"
                    if [ -n "$prev" ]; then
                        buf breaking --against ".git#tag=${prev}"
                    fi
                '''
            }
        }

        stage('Buf Generate') {
            agent {
                docker {
                    image 'bufbuild/buf:1.68.1'
                    reuseNode true
                }
            }
            steps {
                sh 'buf generate'
            }
        }

        stage('Verify Generated Sync') {
            steps {
                sh 'git diff --exit-code gen/go/'
            }
        }

        stage('Verify Go') {
            agent {
                docker {
                    image 'golang:1.24-alpine'
                    reuseNode true
                    args '-v $HOME/go-modcache:/go/pkg/mod'
                }
            }
            steps {
                sh 'go build ./...'
                sh 'go vet ./...'
            }
        }

        stage('Verify Java') {
            agent {
                docker {
                    image 'eclipse-temurin:21-jdk-alpine'
                    reuseNode true
                    args '-v $HOME/.gradle:/root/.gradle'
                }
            }
            steps {
                sh './gradlew --no-daemon compileJava'
            }
        }

        stage('Publish Java') {
            when { buildingTag() }
            agent {
                docker {
                    image 'eclipse-temurin:21-jdk-alpine'
                    reuseNode true
                    args '-v $HOME/.gradle:/root/.gradle'
                }
            }
            steps {
                script {
                    def tagName = env.TAG_NAME
                    if (!(tagName ==~ /^v\d+\.\d+\.\d+(-[\w.]+)?$/)) {
                        error "invalid tag: ${tagName}"
                    }
                    def version = tagName.replaceFirst('^v', '')

                    withCredentials([usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )]) {
                        sh "./gradlew --no-daemon publish " +
                           "-Pversion=${version} " +
                           "-Pnexus.username=\$NEXUS_USERNAME " +
                           "-Pnexus.password=\$NEXUS_PASSWORD"
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
