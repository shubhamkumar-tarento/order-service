
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
        DOCKER_USERNAME = 'shubhamkumar55'
        IMAGE_NAME = 'order-service'
        IMAGE_TAG = "${BUILD_NUMBER}"
        IMAGE = "${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}"
    }

    stages {

        stage('Checkout GitHub') {
            steps {
                echo 'Checking out source code from GitHub...'
                checkout scm
            }
        }

        stage('Build Spring Boot') {
            steps {
                echo 'Building and testing Spring Boot application...'

                sh 'chmod +x ./mvnw'
                sh './mvnw -B clean verify'
            }

            post {
                always {
                    junit(
                        testResults: 'target/surefire-reports/*.xml',
                        allowEmptyResults: true
                    )

                    archiveArtifacts(
                        artifacts: 'target/*.jar',
                        fingerprint: true,
                        allowEmptyArchive: true
                    )
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Building Docker image: ${IMAGE}"

                sh """
                    docker build -t ${IMAGE} -t ${DOCKER_USERNAME}/${IMAGE_NAME}:latest .
                """
            }
        }

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
                    sh """
                        echo "\$DOCKER_TOKEN" | docker login \
                        --username "\$DOCKER_USER" \
                        --password-stdin
                    """
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                echo "Pushing ${IMAGE} to Docker Hub..."

                sh "docker push ${IMAGE}"
                sh "docker push ${DOCKER_USERNAME}/${IMAGE_NAME}:latest"
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo "Deploying ${IMAGE} to Kubernetes..."

                withCredentials([
                    file(
                        credentialsId: 'kubeconfig-credentials',
                        variable: 'KUBECONFIG'
                    )
                ]) {

                    sh """
                        sed -E 's#^([[:space:]]*)image:[[:space:]].*#\\1image: ${IMAGE}#' \
                        k8s/deployment.yaml > k8s/deployment.rendered.yaml

                        echo "--- rendered image line ---"
                        grep "image:" k8s/deployment.rendered.yaml
                    """

                    sh """
                        kubectl -n ${params.K8S_NAMESPACE} \
                        apply -f k8s/deployment.rendered.yaml
                    """

                    sh """
                        kubectl -n ${params.K8S_NAMESPACE} \
                        apply -f k8s/service.yaml
                    """

                    sh """
                        kubectl -n ${params.K8S_NAMESPACE} \
                        rollout status deployment/order-service \
                        --timeout=180s
                    """
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
            sh 'docker logout || true'
            cleanWs()
        }
    }
}

