# NanoJ-Core - ImageJ Plugin #

**NanoJ-Core** is an open-source ImageJ and Fiji plugin that provide the base high-performance computing engine that other NanoJ plugins need to run: [NanoJ-SRRF](https://github.com/HenriquesLab/NanoJ-SRRF), [NanoJ-SQUIRREL](https://bitbucket.org/rhenriqueslab/nanoj-squirrel/wiki/Home), [NanoJ-VirusMapper](https://bitbucket.org/rhenriqueslab/nanoj-virusmapper/wiki/Home) and [NanoJ-Fluidics](https://github.com/HenriquesLab/NanoJ-Fluidics).

**NanoJ-Core** also provides image analysis methods for **Drift Correction** and **Channel Alignment**. Check out our [**bioRxiv preprint**](https://www.biorxiv.org/content/early/2018/10/01/432674) for a full description of the NanoJ features, and the [**User Manual**](https://doi.org/10.6084/m9.figshare.7296767.v1) for an in-depth guide on how to use them.

[![NanoJ Pre-Print](https://pbs.twimg.com/media/Doez3mnXsAA_RxN.jpg "NanoJ Pre-Print")](https://www.biorxiv.org/content/early/2018/10/01/432674)


## Install NanoJ-Core ##


NanoJ-Core is available as an ImageJ/Fiji (from here on ImageJ) plugin. The installation proceeds thought the ImageJ updater: open ImageJ and select “Update” in the “Help”-menu.  

![alt text](https://user-images.githubusercontent.com/34708291/46486258-45659400-c7f5-11e8-9b31-acb07da686d4.png)  
Regardless if other updates are available or not, press the “Manage update sites”-button on the Updater window (indicated in red).  
![alt text](https://user-images.githubusercontent.com/34708291/46486262-47c7ee00-c7f5-11e8-9045-5fe2e10246c4.png)  
Then, scroll down the list of update sites, click on the tick-box next to “NanoJ-Core" and click the “Close” button (in red).  
![alt text](https://user-images.githubusercontent.com/34708291/46486269-4c8ca200-c7f5-11e8-9585-0d64bd22276a.png)  
Finally, click “Apply changes” (in red) in the Updater window, close and then restart ImageJ.  
![alt text](https://user-images.githubusercontent.com/34708291/46486272-4e566580-c7f5-11e8-8304-c16d093bf883.png)  
You can find NanoJ-Core under the “Plugins” menu.  
![alt text](https://user-images.githubusercontent.com/34708291/46486274-4f879280-c7f5-11e8-8947-d69e94d90004.png)

## Image Registration methods in Python ##

Exciting news! Drift alignment and Channel registration are now accessible in Python through the [NanoPyx](https://github.com/HenriquesLab/NanoPyx) package. This integration brings the power and versatility of NanoJ-Core to Python users, opening up new possibilities for analysis and integration within Python-based workflows.

NanoPyx seamlessly integrates NanoJ-Core's image registration capabilities into Python environments. With NanoPyx, users can now leverage NanoJ's high-performance analytical approach within their Python scripts, pipelines, and interactive sessions. Through NanoPyx, NanoJ-Core image registration methods are also available as "codeless" Jupyter Notebooks and a [napari plugin](https://github.com/HenriquesLab/napari-NanoPyx).

