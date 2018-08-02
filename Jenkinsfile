@Library('pipeline-lib') _
pipeline {
    agent any

    parameters {
        string(name: 'fasitEnvPreprod', defaultValue: 'q1', description: 'Fasit environment used for reading and exposing resources (preprod)')
        string(name: 'namespacePreprod', defaultValue: 'default', description: 'Nais namespace (preprod)')
        booleanParam(name: 'gatling', defaultValue: true, description: 'Whether to run Gatling tests as part of the build')
    }

    environment {
        APPLICATION_NAME = 'sak'
        APPLICATION_VERSION = version()
        APPLICATION_SERVICE = 'gosys'
        APPLICATION_COMPONENT = 'sak'
        FASIT_ENV = "${params.fasitEnvPreprod}"
        NAMESPACE = "${params.namespacePreprod}"
        RUN_GATLING = "${params.gatling}"
    }
    tools {
        maven "maven3"
        jdk "java8"
    }

    options {
        timestamps()
    }

    stages {
        stage('Maven Build (unit & integration)') {
            steps {
                script {
                    withCredentials([
                        usernamePassword([credentialsId: 'junit.sts', usernameVariable: 'junit.sts.user', passwordVariable: 'junit.sts.password']),
                        usernamePassword([credentialsId: 'systembruker', usernameVariable: 'SRVSAK_USERNAME', passwordVariable: 'SRVSAK_PASSWORD']),
                        usernamePassword([credentialsId: 'sak-t0', usernameVariable: 'isso-rp-issuer', passwordVariable: 'OpenIdConnectAgent.password']),
                        usernamePassword([credentialsId: 'ldap', usernameVariable: 'LDAP_USERNAME', passwordVariable: 'LDAP_PASSWORD']),
                        string(credentialsId: 'truststore-password', variable: 'truststore.password')
                    ]) {
                        sh "mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install -Pintegration-tests"
                    }
                }
            }
        }

        stage('Transfer build results to SonarQube') {
            steps {
                script {
                    withSonarQubeEnv('SonarQube') {
                        sh "mvn sonar:sonar"
                    }
                }
            }
        }

        stage('Verify dependencies with owasp dependency-checker') {
            steps {
                dependencyCheck()
            }
        }

        stage('Build and push docker image') {
            steps {
                dockerUtils 'buildAndPush'
            }
        }

        stage('Validate & upload nais.yaml to nexus') {
            steps {
                nais action: 'validate'
                nais action: 'upload'
            }
        }

        stage('Deploy to nais preprod') {
            steps {
                script {
                    def jiraIssueId = nais action: 'jiraDeploy'
                    nais action: 'waitForCallback'
                    slack status: 'deployed', jiraIssueId: "${jiraIssueId}"
                }
            }
        }

        stage('Run gatling-tests') {
            when { environment name: 'RUN_GATLING', value: 'true' }
            steps {
                script {
                    withCredentials([
                        usernamePassword([credentialsId: 'junit.sts', usernameVariable: 'junit.sts.user', passwordVariable: 'junit.sts.password']),
                        usernamePassword([credentialsId: 'sak-t0', usernameVariable: 'isso-rp-issuer', passwordVariable: 'OpenIdConnectAgent.password']),
                        usernamePassword([credentialsId: 'sakds.lasttest', usernameVariable: 'sakds.lasttest.user', passwordVariable: 'sakds.lasttest.password']),
                        usernamePassword([credentialsId: 'systembruker', usernameVariable: 'SRVSAK_USERNAME', passwordVariable: 'SRVSAK_PASSWORD']),
                        string(credentialsId: 'truststore-password', variable: 'truststore.password')
                    ]) {
                        sh "mvn gatling:test"
                    }
                }
            }
        }
        stage('Deploy to nais prod') {
            when { branch 'master' }
            environment {
                FASIT_ENV = 'p'
                NAMESPACE = 'default'
            }
            steps {
                script {
                    tag()
                    def jiraIssueId = nais action: 'jiraDeployProd'
                    nais action: 'waitForCallback'
                    slack status: 'deployed', jiraIssueId: "${jiraIssueId}"
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
            junit 'target/surefire-reports/*.xml'
            gatlingArchive()

            script {
                if (currentBuild.result == 'ABORTED') {
                    slack status: 'aborted'
                }
            }
            dockerUtils 'prune'
            deleteDir()
        }
        success {
            slack status: 'success'
        }
        failure {
            slack status: 'failure'
        }
    }

}
