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

    // Step 2: Poll for running builds and wait for them to finish
    def isIdle = false
    while (!isIdle) {
        def runningBuilds = Jenkins.instance.getComputers().collectMany { it.executors }
            .findAll { it.currentExecutable != null && it.name == NODE_NAME }
        
        echo "Running builds on ${NODE_NAME}: ${runningBuilds.size()}"
        
        if (runningBuilds.isEmpty()) {
            isIdle = true
        } else {
            echo "Waiting for builds to finish on ${NODE_NAME}..."
            sleep(time: 60, unit: 'SECONDS') // Wait for a minute before checking again
        }
    }
    
    if (isIdle) {
        echo "No other builds are running on the node for the Cleanup_Job."
    }
    
    sleep(time: 60, unit: 'SECONDS')
    
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
