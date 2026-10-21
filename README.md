[![Release](https://jitpack.io/v/umjammer/arcoflex056.svg)](https://jitpack.io/#umjammer/arcoflex056)
[![Java CI](https://github.com/umjammer/arcoflex056/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/arcoflex056/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/arcoflex056/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/arcoflex056/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-25-b07219)

# arcoflex056

<img alt="logo" src="src/test/resources/duke_mame.png" width="160" />

Arcadeflex and Consoleflex merged

this is a fork of [original](https://github.com/javaemus/arcoflex056).

## Install

* [maven](https://jitpack.io/#umjammer/arcoflex056)

## Usage

 * edit `arcadeflex056/settings.java`

```java
    public static String installationDir = "/usr/local/games/mame/";
```

 * old roms for version 0.56 roms are needed 

### amame

[amame](https://github.com/jariseon/amame) (mame's synthesizers as VST2 plug-ins) ported to java,
in the `amame` package.

| machine | name           | status | comment                                                                   |
|---------|----------------|:------:|---------------------------------------------------------------------------|
| fb01    | Yamaha FB-01   |   ✅    | Z80, YM2164, uPD71051, HD44780                                            |
| vfx     | Ensoniq VFX    |   ✅    | 68000, ES5505, ES5510, MC68681, the vfd panel with its lights, cartridges |
| vfxsd   | Ensoniq VFX-SD |   ✅    | as the VFX, WD1772 and the floppy drive                                   |

 * the roms of mame 0.289: `fb01`, `vfx`, `vfxsd`, `hd44780`, `esq2x40_vfx`, the directory
   goes to the system property `amame.rompath` (`amame.rompath` in `local.properties` for the tests)
 * standalone, the panel, the virtual keyboard and every midi input of the system
 * disks are ensoniq images (`.img`, 800KiB, mame's esq16), cartridges are 32KiB files,
   `.eeprom` and `.sc32` are written back, `.rom` and `.cart` are read only. "New" makes a blank
   one, a disk is formatted by the VFX-SD itself (storage → disk → format)

```shell
$ mvn -P amame antrun:run -Damame.rompath=/path/to/roms -Damame.machine=vfx
```

 * VST2: `amame.plugin.AmamePlugin` builds a real `AEffect` whose function pointers are jna callbacks,
   a java host takes it in process with `org.urish.jnavst.VstPlugin(VstEffect, VstHost)`
   (jnavst is from vavi-apps-mdplayer). a native DAW would need a small library that starts a jvm.
 * the cpus are those of vavi-sound-m1 (Z80A, MC68000), the 68000 has had its port bugs fixed against mame

## References

 * roms
   * https://wowroms.com/
   * https://www.planetemu.net/
   * https://ia600303.us.archive.org/view_archive.php?archive=/33/items/128gb/Roms.zip (for old mame)
   * https://www.zophar.net/pdroms.html (public domain)

## TODO

 * jinput
 * merge [vavi-sound-m1](https://jitpack.io/v/umjammer/vavi-sound-m1)

---

<sub>image designed by @umjammer, drawn by nano banana</sub>
