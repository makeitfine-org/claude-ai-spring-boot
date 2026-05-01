module.exports = {
  default: {
    require: ['src/**/*.ts'],
    requireModule: ['ts-node/register'],
    format: [
      'progress-bar',
      'html:reports/cucumber.html',
      'json:reports/cucumber.json'
    ],
    paths: ['features/**/*.feature'],
    tags: 'not @wip',
    parallel: 1
  }
}
