pipeline {
    agent {
        label 'rdevara'
    }

    triggers {
        cron('H/30 * * * *')
    }

    stages {
        stage('Check Disk Space') {
            steps {
                script {
                    def nodeInfo = env.NODE_NAME
                    def diskSpaceOutput = sh(script: "df -h /tmp | awk 'NR==2 {print \$5}'", returnStdout: true).trim()
                    echo "Disk Space Output: ${diskSpaceOutput}" // Print the diskSpaceOutput

                    def usageIndex = diskSpaceOutput.indexOf('%')
                    if (usageIndex > -1) {
                        def diskSpacePercentage = diskSpaceOutput[usageIndex - 2..usageIndex - 1] as Integer
                        if (diskSpacePercentage > 70) {
                            currentBuild.description = "Disk Space Usage: ${diskSpacePercentage}% Triggering clean up job"
                            echo "Disk space exceeds 80%. Triggering post-build job..."
                            build job: 'Jenkins_space', parameters: [
                                string(name: 'node_name', value: env.NODE_NAME),
                                string(name: 'node_ip_address', value: InetAddress.getLocalHost().getHostAddress())
                            ]
                        } else {
                            echo "Disk space looks good. No further action required"
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo "The disk space check was successful."
        }
        
        failure {
            echo "The disk space check failed."
        }
    }
}
