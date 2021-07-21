const childProcess = require('child_process')

const version = childProcess
    .execSync('./gradlew properties -q | grep "version:" | awk \'{print $2}\'', {
        cwd: '../../../'
    })
    .toString().trim()
const gitBranch = childProcess
    .execSync('git branch --show-current')
    .toString().trim()

module.exports = {
    version: version,
    branch: gitBranch
}
