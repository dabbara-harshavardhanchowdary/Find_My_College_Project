// Re-export master mobile tests from parent directory
module.exports = require('../app-tests.js');

if (require.main === module) {
    const { runMobileAppiumE2ETests } = require('../app-tests.js');
    runMobileAppiumE2ETests().catch(err => {
        console.error('Mobile test execution failed:', err);
        process.exit(1);
    });
}
