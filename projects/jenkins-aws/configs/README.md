# Jenkins Configuration as Code (JCasC)

Simple, single-file configuration.

## Setup

Set the environment variable to point to this file:
```bash
export CASC_JENKINS_CONFIG=/path/to/jenkins-iac/jcasc.yaml
```

Or point to the directory (JCasC will find all YAML files):
```bash
export CASC_JENKINS_CONFIG=/path/to/jenkins-iac
```

## File

- `jcasc.yaml` - Complete Jenkins configuration (system, tools, plugins)

That's it!
