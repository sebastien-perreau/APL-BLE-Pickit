# APL_BLEPickit

[![](https://jitpack.io/v/sebastien-perreau/APL-BLE-Pickit.svg)](https://jitpack.io/#sebastien-perreau/APL-BLE-Pickit)

## Importing

#### Maven Central

The library may be found on Maven Central repository. 

First add the JitPack repository to your build file:

```grovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Then add it to your project by adding the following dependency:

```grovy
dependencies {
		implementation 'com.github.sebastien-perreau:APL-BLE_Pickit:0.9.0'
	}
```

#### As a library module

Clone this project and add *blepickit* module as a dependency to your project:

1. In your (Project Settings) *settings.gradle* file add the following lines:
```groovy
include ':blepickit'
project(':blepickit').projectDir = file('../APL_BLEPickit/blepickit')
```
2. In *app/build.gradle* file add `implementation project(':blepickit')` inside dependencies.
3. Sync project and build it.
 
