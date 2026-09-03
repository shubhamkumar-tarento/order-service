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

    // Jenkins build number used as Docker image tag
    IMAGE_TAG = "${BUILD_NUMBER}"

    // Final Docker image
    IMAGE = "${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}"
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
    // 2. Build & test Spring Boot
    // =========================================================
    stage('Build Spring Boot') {
        steps {
            echo 'Building and testing Spring Boot application...'

            // Maven wrapper pins the Maven version, so the agent only
            // needs a JDK on PATH. `verify` runs the test suite.
            sh 'chmod +x ./mvnw'

            sh './mvnw -B clean verify'
        }

        post {
            always {
                junit(
                    testResults: 'target/surefire-reports/*.xml',
                    allowEmptyResults: false
                )

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

            // Multi-stage Dockerfile compiles the jar itself, so the build
            // context does not depend on the target/ directory above.
            sh """
                docker build \
                -t ${IMAGE} \
                -t ${DOCKER_USERNAME}/${IMAGE_NAME}:latest .
            """
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
                sh '''
                    echo "$DOCKER_TOKEN" | docker login \
                    --username "$DOCKER_USER" \
                    --password-stdin
                '''
            }
        }
    }


    // =========================================================
    // 5. Push Docker Image
    // =========================================================
    stage('Push Docker Image') {
        steps {
            echo "Pushing ${IMAGE} to Docker Hub..."

            sh "docker push ${IMAGE}"

            sh "docker push ${DOCKER_USERNAME}/${IMAGE_NAME}:latest"
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

                // Pin this build's tag into the manifest BEFORE applying, so
                // the cluster never briefly runs the checked-in :latest tag
                // and a later re-apply cannot undo the rollout.
                sh '''
                    sed -E "s#^([[:space:]]*)image:[[:space:]].*#\\1image: ${IMAGE}#" \
                    k8s/deployment.yaml > k8s/deployment.rendered.yaml

                    echo "--- rendered image line ---"
                    grep "image:" k8s/deployment.rendered.yaml
                '''

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


// =============================================================
// Pipeline result
// =============================================================
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
        // Drop the registry credentials from the agent's docker config.
        sh 'docker logout || true'

        cleanWs()
    }
}

}
