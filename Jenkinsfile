@Library('nais') _
pipeline {
    agent any
    environment {
        versjon = "0"
    }
    tools {
        maven "maven3"
        jdk "java8"
    }

    options {
        timestamps()
    }

    stages {
        stage("Checkout") {
            steps {
                script {
                    def commit = env.GIT_COMMIT
                    def gitCommitHashShort = commit.substring(0, 8)
                    versjon = currentDate() + "." + gitCommitHashShort
                }
            }
        }
        stage('Maven build') {
            steps {
                script {
                    withCredentials([
                        usernamePassword([credentialsId: 'systembruker', usernameVariable: 'testLoginUsername', passwordVariable: 'testLoginPassword']),
                        usernamePassword([credentialsId: 'systembruker', usernameVariable: 'SRVSAK_USERNAME', passwordVariable: 'SRVSAK_PASSWORD']),
                        usernamePassword([credentialsId: 'sak-t0', usernameVariable: 'isso-rp-issuer', passwordVariable: 'OpenIdConnectAgent.password']),
                        usernamePassword([credentialsId: 'ldap', usernameVariable: 'LDAP_USERNAME', passwordVariable: 'LDAP_PASSWORD'])
                    ]){
                        if (env.BRANCH_NAME == 'master') {
                            sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install -Pmutation-tests,integration-tests"
                        } else {
                            sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install -Pintegration-tests"
                        }
                    }
                }
            }
        }

        stage('SonarQube') {
            steps {
                script {
                    withSonarQubeEnv('SonarQube') {
                        sh "mvn sonar:sonar"
                    }
                }
            }
        }
        stage('Owasp Dependency Check') {
            steps {
                script {
                    dependencyCheckAnalyzer datadir: 'dependency-check-data',
                        isFailOnErrorDisabled: true,
                        hintsFile: '',
                        includeCsvReports: false,
                        includeHtmlReports: false,
                        includeJsonReports: false,
                        isAutoupdateDisabled: false,
                        outdir: '',
                        scanpath: '',
                        skipOnScmChange: false,
                        skipOnUpstreamChange: false,
                        suppressionFile: 'owasp-suppression.xml',
                        zipExtensions: ''

                    dependencyCheckPublisher canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '', unHealthy: ''
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/dependency-check-report.xml', onlyIfSuccessful: true
                }

            }
        }

        stage('Docker build') {
            steps {
                milestone(1)
                dockerBuild("sak", versjon)
            }
        }
        stage('Nais upload') {
            steps {
                milestone(2)
                naisUpload("sak", versjon)
            }
        }
        stage('Nais deploy (preprod - lasttest)') {
            steps {
                milestone(3)
                script {
                      if (env.BRANCH_NAME == 'master') {
                           naisDeployPreprod("sak", versjon, "t8", "t8")
                      } else {
                         echo "Last-tester kjører kun på master. Deploy ikke utført"}
                }
            }
        }

        stage('Run Gatling Tests') {
            steps {
                milestone(4)
                withCredentials([
                    usernamePassword([credentialsId: 'systembruker', usernameVariable: 'testLoginUsername', passwordVariable: 'testLoginPassword']),
                    usernamePassword([credentialsId: 'sak-t0', usernameVariable: 'isso-rp-issuer', passwordVariable: 'OpenIdConnectAgent.password']),
                    usernamePassword([credentialsId: 'systembruker', usernameVariable: 'systembruker.username', passwordVariable: 'systembruker.password']),
                    usernamePassword([credentialsId: 'sakds.lasttest', usernameVariable: 'sakds.lasttest.user', passwordVariable: 'sakds.lasttest.password']),
                    string(credentialsId: 'truststore-password', variable: 'truststore.password')
                ]){
                     script {
                         if (env.BRANCH_NAME == 'master') {
                             sh "mvn gatling:test"
                         } else {
                             echo "Last-tester kjører kun på master"}
                         }
                }
            }
        }

        stage('Nais deploy (preprod - default)') {
            steps {
                milestone(5)
                naisDeployPreprod("sak", versjon, "u1")
                slackSend (color: '#90ee90', message: "Deployet til preprod: ${env.BRANCH_NAME} Sak:" + versjon)
            }
        }

        stage('Nais Deploy (prod)') {
            steps {
                milestone(6)
                script {
                    if (env.BRANCH_NAME == 'master') {
                        naisDeployProd("sak", versjon)
                        slackSend (color: '#006400', message: "Deployet til produksjon: ${env.BRANCH_NAME} Sak:" + versjon)
                    } else {
                        currentBuild.description = "OK - ikke deployet til prod"
                        echo "Kun master blir deployet til prod"
                    }
                }
            }
        }
    }

    post {
        failure {
            slackSend (color: '#FF0000', message: "Bygget feilet: ${env.BRANCH_NAME} ${env.BUILD_URL}")
        }
        always {
            junit 'target/surefire-reports/*.xml'
            gatlingArchive()
        }
    }
}
