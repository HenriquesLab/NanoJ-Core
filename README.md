# NanoJ-Core - ImageJ Plugin #

**News** - we will soon add a manual for the **Drift Correction** and **Channel Alignment** features.

The super-resolution microscopy field is now populated by a rich collection of data analysis packages based on Matlab, Python, Java and C/C++ approaches among others. However, the Java based [ImageJ](http://imagej.net/Welcome) remains the most popular frontend for image based analysis among biologists. 

In 2010 we published the first open-source ImageJ based 3D SMLM analysis package – [QuickPALM](http://www.nature.com/nmeth/journal/v7/n5/full/nmeth0510-339.html) – rapidly becoming one of the most popular algorithms in the field, due to its simplicity of use, speed and ImageJ integration. Based on the same philosophy, we have been developing a new analytical engine for ImageJ, named NanoJ, capable of seamlessly providing the capacity to run high-performance computing super-resolution data analysis by dynamically switching between CPU and GPU massive parallelisation, but remaining extremely easy to use by non-specialised researchers. 

NanoJ has now allowed us to achieve a new set of analytical tools for super-resolution, as is the case 
of [NanoJ-SRRF](https://bitbucket.org/rhenriqueslab/nanoj-srrf) – the successor of QuickPALM, capable of enabling super-resolution in modern conventional microscopes using standard fluorophores such as GFP; and [NanoJ-VirusMapper](https://bitbucket.org/rhenriqueslab/nanoj-virusmapper), a high-performance single-particle analysis algorithm to model stable supra-molecular assemblies with super-resolution microscopy. 

## About NanoJ-Core ##

**NanoJ-Core** and **[NanoJ-Updater](https://bitbucket.org/rhenriqueslab/nanoj-updater)** provide the base high-performance computing engine that other NanoJ plugins (e.g.: SRRF, VirusMapper) need to run. **NanoJ** is developed by the [Henriques laboratory](http://www.ucl.ac.uk/lmcb/users/ricardo-henriques) in the [MRC Laboratory for Molecular Cell Biology](http://www.ucl.ac.uk/lmcb/) at [University College London](http://www.ucl.ac.uk/).
