Installation

Eclipse Modeling tools

Acceleo 4.2

<https://download.eclipse.org/acceleo/updates/releases/4.2/R202602100910/>

Select all

Help / Install New Software

2026-03 download site (already configured)

Multipe searches for 'web developer', 'm2e' (check item after each
search)

Eclipse Web Developer Tools

Eclipse Web Developer Tools -- JavaScript Support

Wild Web Developer HTML, CSS, JSON, Yaml, JavaScript, Node tools

Wild Web Developer XML tools

M2E -- Maven Integration for Eclipse

M2E -- PDE Integration

M2E -- POM Editor using LemMiX \...

Window / Show View / Other / General

Console, Error Log

Import Maven Project

File / Import \... / Maven / Existing Maven Projects

/media/wackerow/Shared/Git/github/ucmism2t

Installs automatically: m2e connectors

maven-clean-plugin:3.2.0:clean (Action: Install Tycho Configurator
bridges ...)

results in component: Tycho Project Configurators
(org.sonatype.tychoe.m2e.feature.feature.group)

Cancel (important)

otherwise:

Errors because of two different m2e connectors both claim to handle
eclipse-plugin packaging --- the *old Tycho m2e connector
(org.sonatype.tycho.m2e)* and the newer PDE connector
(org.eclipse.m2e.pde.connector). Remove the old connector.

Help / About Eclipse IDE / Installation Details / Installed Software

Search for 'Tycho Project Configurators' (id:
org.sonatype.tycho.m2e.feature.feature.group)

There are three error messages:

Unknown extension point: \'org.eclipse.acceleo.query.services\'
plugin.xml /ucmism2t.core line 37 Plug-in Problem

Unknown extension point: \'org.eclipse.acceleo.query.services\'
plugin.xml /ucmism2t.core line 45 Plug-in Problem

Unknown extension point: \'org.eclipse.acceleo.query.services\'
plugin.xml /ucmism2t.core line 53 Plug-in Problem

There is no solution found for this but a generation can be run without
issues.

Changing these error messages to information messages:

Windows / Preferences / Plug-in development / Compilers / Configure
Project Specific Settings ...

Select ucmism2t.core

Check 'Enable project specific settings'

Plug-ins / Build / Unresolved extension points -- set to 'Warning'

Apply and Close

Project / Clean

The messages are now Warnings.

Activate target platform (important)

Window / Preferences / Plug-in Development / Target Platform

Check 'ucmism2t Target Platform (parent)

Activate target platform

Window / Preferences / Plug-in Development / Target Platform / Check
'Combined Acceleo + QVTo Platform

Error Log Window: delete all events

Problems Window: delete all entries

Select all projects

right-click / Maven / Update Project / Check 'Force Update of
Snapshots/Releases

Project / Clean
