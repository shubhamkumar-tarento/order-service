```groovy
// Runs commands on both Linux and Windows Jenkins agents.
def run(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        string(
            name: 'K8S_NAMESPACE',
            defaultValue: 'default',
            description: 'Kubernetes namespace'
        )
    }

    environment {
        // Docker Hub username
        DOCKER_USERNAME = 'shubhamkumar55'

        // Docker Hub repository
        IMAGE_NAME = 'order-service'

        // Docker image tag = Jenkins build number
        IMAGE_TAG = "${BUILD_NUMBER}"

        // Final Docker image
        IMAGE = "${DOCKER_USERNAME}/${IMAGE_NAME}:${BUILD_NUMBER}"
    }

    stages {

        // =========================================================
        // 1. Checkout GitHub
        // =========================================================
        stage('Checkout GitHub') {
            steps {
                echo 'Checking out source code from GitHub...'

                checkout scm
            }
        }


        // =========================================================
        // 2. Build Spring Boot
        // =========================================================
        stage('Build Spring Boot') {
            steps {
                echo 'Building Spring Boot application...'

                run 'mvn -B clean package -DskipTests'
            }

            post {
                always {
                    archiveArtifacts(
                        artifacts: 'target/*.jar',
                        fingerprint: true,
                        allowEmptyArchive: true
                    )
                }
            }
        }


        // =========================================================
        // 3. Build Docker Image
        // =========================================================
        stage('Build Docker Image') {
            steps {
                echo "Building Docker image: ${IMAGE}"

                run "docker build -t ${IMAGE} -t ${DOCKER_USERNAME}/${IMAGE_NAME}:latest ."
            }
        }


        // =========================================================
        // 4. Login to Docker Hub
        // =========================================================
        stage('Login to Docker Hub') {
            steps {
                echo 'Logging in to Docker Hub...'

                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker-registry-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_TOKEN'
                    )
                ]) {
                    run 'echo %DOCKER_TOKEN% | docker login -u %DOCKER_USER% --password-stdin'
                }
            }
        }


        // =========================================================
        // 5. Push Docker Image
        // =========================================================
        stage('Push Docker Image') {
            steps {
                echo "Pushing ${IMAGE} to Docker Hub..."

                run "docker push ${IMAGE}"

                run "docker push ${DOCKER_USERNAME}/${IMAGE_NAME}:latest"
            }
        }


        // =========================================================
        // 6. Deploy to Kubernetes
        // =========================================================
        stage('Deploy to Kubernetes') {
            steps {
                echo "Deploying ${IMAGE} to Kubernetes..."

                withCredentials([
                    file(
                        credentialsId: 'kubeconfig-credentials',
                        variable: 'KUBECONFIG'
                    )
                ]) {

                    run "kubectl -n ${params.K8S_NAMESPACE} apply -f k8s/deployment.yaml"
                    run "kubectl -n ${params.K8S_NAMESPACE} apply -f k8s/service.yaml"

                    run "kubectl -n ${params.K8S_NAMESPACE} set image deployment/order-service order-service=${IMAGE}"

                    run "kubectl -n ${params.K8S_NAMESPACE} rollout status deployment/order-service --timeout=180s"
                }
            }
        }
    }

    post {

        success {
            echo "=============================================="
            echo "PIPELINE COMPLETED SUCCESSFULLY"
            echo "Docker Image: ${IMAGE}"
            echo "Kubernetes Namespace: ${params.K8S_NAMESPACE}"
            echo "=============================================="
        }

        failure {
            echo "=============================================="
            echo "PIPELINE FAILED"
            echo "Check the failed stage in Jenkins Console Output."
            echo "=============================================="
        }

        always {
            cleanWs()
        }
    }
}
```
