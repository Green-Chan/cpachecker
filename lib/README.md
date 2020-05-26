Libraries Used by CPAchecker
============================

Binary Libraries
----------------

Binary libraries should be provided via Ivy.

All libraries in the directory `lib/java/` are managed by Apache Ivy.
To add libraries there, add them to the file `ivy.xml`.
Do not store any file in that directory, Ivy will delete it!
To generate a report listing all libraries managed by Ivy,
call `ant report-dependencies`.

- `cbmc`: [CBMC](http://www.cprover.org/cbmc/)  
  Open-source license: `license-cbmc.txt`  
  Bit-precise bounded model checker for C

- `libJOct.so`: [Octagon Abstract Domain Library](http://www.di.ens.fr/~mine/oct/)  
  Octagon-Abstract-Domain License: `license-octagon.txt`  
  Used for octagon abstract domain  
  Source for wrapper in `native/source/octagon-libJOct.so/`

- `jsylvan.jar` and `libsylvan.so`:
  [Sylvan](http://fmt.ewi.utwente.nl/tools/sylvan/)
  and its [Java bindings](https://github.com/trolando/jsylvan)  
  Apache 2.0 License  
  BDD package for multi-core CPUs  
  Manual for building in `native/source/libsylvan.md`

- `jpl.jar` and `libjpl.so`: [SWI-PL](http://www.swi-prolog.org/)  
  Lesser GNU Public License

- `chc_lib`: CHC/CLP Generalization Operators Library  
  It requires a working SWI-Prolog installation.  
  Apache License, Version 2.0