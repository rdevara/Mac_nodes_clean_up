node('master') {
    def NODE_NAME = params.NODE_NAME ?: 'rdevara'
    echo "Node Name is: ${NODE_NAME}"
    
    // Step 1: Take the Jenkins node offline
    try {
        def node = Jenkins.instance.getNode(NODE_NAME)
        if (!node.toComputer().isOffline()) {
            node.toComputer().disconnect()
            echo "Node is offline for the Cleanup_Job."
        } else {
            echo "Node is already offline for the Cleanup_Job."
        }
    } catch (Exception e) {
        error "Failed to take the node offline for the Cleanup_Job: ${e.message}"
    }
    
    // Step 2: Poll for running jobs
    def isIdle = false
    while (!isIdle) {
        def runningJobs = Jenkins.instance.getItems(hudson.model.AbstractBuild.class)
            .findAll { it.building && it.builtOnStr == NODE_NAME }
        
        if (runningJobs.isEmpty()) {
            isIdle = true
        } else {
            echo "Waiting for builds to finish on ${NODE_NAME}..."
            sleep(time: 60, unit: 'SECONDS') // Wait for a minute before checking again
        }
    }
    
    // Step 3: Bring the Jenkins node back online
    try {
        def node = Jenkins.instance.getNode(NODE_NAME)
        if (node.toComputer().isOffline()) {
            node.toComputer().setAcceptingTasks(true)
            echo "Node is back online for the Cleanup_Job."
        } else {
            echo "Node was already offline for the Cleanup_Job."
        }
    } catch (Exception e) {
        error "Failed to bring the node back online for the Cleanup_Job: ${e.message}"
    }
}
