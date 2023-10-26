node('master') {
    def NODE_NAME = params.NODE_NAME ?: 'rdevara'
    echo "Node Name is: ${NODE_NAME}"
    
    // Step 1: Disable the Jenkins node
    try {
        def node = Jenkins.instance.getNode(NODE_NAME)
        if (!node.toComputer().isOffline()) {
            node.toComputer().setTemporarilyOffline(true, null)
            echo "Node is temporarily marked offline for the Cleanup_Job."
        } else {
            echo "Node is already offline for the Cleanup_Job."
        }
    } catch (Exception e) {
        error "Failed to mark the node temporarily offline for the Cleanup_Job: ${e.message}"
    }

    // Step 2: Check for running builds and wait for them to finish
    def runningJobCount = 1
    def maxWaitTimeInSeconds = 3600 // Adjust as needed (e.g., one hour)

    while (runningJobCount > 0 && maxWaitTimeInSeconds > 0) {
        runningJobCount = 0

        Jenkins.instance.nodes.each { jenkinsNode ->
            if (jenkinsNode.getNodeName() == NODE_NAME) {
                runningJobCount += jenkinsNode.toComputer().executors.count { executor ->
                    def build = executor.getCurrentExecutable()
                    build != null
                }
            }
        }

        if (runningJobCount > 0) {
            echo "Waiting for $runningJobCount builds to finish on ${NODE_NAME}..."
            sleep(time: 60, unit: 'SECONDS') // Wait for a minute before checking again
            maxWaitTimeInSeconds -= 60
        }
    }

    if (runningJobCount == 0) {
        echo "All builds have finished on the node for the Cleanup_Job."
    } else {
        echo "Maximum wait time reached, but some builds are still running on the node."
    }

    // Step 3: Enable the Jenkins node to bring it back online
    try {
        def node = Jenkins.instance.getNode(NODE_NAME)
        if (node.toComputer().isTemporarilyOffline()) {
            node.toComputer().setTemporarilyOffline(false, null)
            echo "Node is back online for the Cleanup_Job."
        } else {
            echo "Node was already temporarily offline for the Cleanup_Job."
        }
    } catch (Exception e) {
        error "Failed to bring the node back online for the Cleanup_Job: ${e.message}"
    }
}
