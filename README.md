# Modelling a potential zoonotic spillover event of H5N1 influenza

The provided code is a simulation written using the agent-based simulation framework _BharatSim_. This simulation is designed to model a potential zoonotic spillover of H5N1 influenza between birds and humans. It allows for a well-mixed population of birds to exist in a single workplace location (designated a "farm") where primary human contacts can be infected. These primary contacts can then spread the infection to the secondary contact network, their household contacts. From here, the disease has the potential to spread to the entire population. We include the possibility of three interventions: the culling of birds on different dates, a vaccination drive that begins a fixed duration after the first detected case, and a lockdown of all farms that is imposed when the number of cases crosses a threshold.

This code was used to produce all the results in the paper "Modelling a potential zoonotic spillover event of H5N1 influenza":
 
 > Cherian, P. and Menon, G. I., Modelling a potential zoonotic spillover event of H5N1 influenza (2025). MedRxiv, 2025.04.28.25326570; doi: https://doi.org/10.1101/2025.04.28.25326570

Questions or comments can be directed to dpcherian@gmail.com, or on this project's GitHub page.

## Requirements

The code is written using the _BharatSim_ framework, coded in _Scala 2_. The _BharatSim_ framework is provided as a library with this code, in the `lib` folder. More information can be found in the documentation of [bharatsim.ashoka.edu.in](https://bharatsim.ashoka.edu.in), or on the [GitHub page for the BharatSim project](https://github.com/bharatsim).

However, this framework both Java and Scala 2 are requirements to run. We recommend using either Java 8 or Java 11, either of which can be obtained from [Oracle](https://www.oracle.com/) or [OpenJDK](https://openjdk.org/). Scala can be installed using _coursier_, the Scala application manager. The installation instructions for coursier can be found on the [coursier site](https://get-coursier.io/docs/cli-installation).

## Description

Individual agents are created from the population stored in `population.csv`. This population is meant to represent a mid-size poultry farm and the secondary and tertiary contacts of those who work there. The total size of the population is 10,000 agents.

## Execution

The code can be executed either in the command line using the `sbt` tool (from Scala) or alternatively, an executable `.jar` java archive can be assembled. See [this section of the BharatSim documentation](https://bharatsim.readthedocs.io/en/latest/miscellaneous.html#assembling-an-executable-jar-file) for instructions on how to create an executable `.jar`. 

The code can be executed in the command line as follows:

### SBT command line:
If you're using the `sbt` command, open a terminal in the project directory and run:

`sbt "run input="./population" output="<outputfolder>/" betabh=<betaBH> betahh=<betaHH> cd=<CD> lf=<lockdownFarmers?> ve=<vaccineEfficacy> dvr=<DVR> vd=<vaccineDelay> simdays=<MAXSIMDAYS>`

### Java command line:
If you're running a compiled java executable, then open a terminal in which the executable is placed and run:
`java -cp <jarname>.jar model.Main input="./population" output="<outputfolder>/" betabh=<betaBH> betahh=<betaHH> cd=<CD> lf=<lockdownFarmers?> ve=<vaccineEfficacy> dvr=<DVR> vd=<vaccineDelay> simdays=<MAXSIMDAYS>`

More arguments can be sent into the simulator using flags. See the `parseArgs` function in the `Main.scala` class of the `model` package for more details.

## Outputs

By default, this project creates a single output file called `agentinfo_<timestamp>` in the directory defined in `Parameters.outputPath`. This output file stores the attributes of all agents in the population who are not susceptible at the end of the simulation. The size of this CSV file thus depends on how many agents were infected. Other output files are also possible, and can be requested for using the `FILES` flag (see the `parseArgs` function of the `Main.scala` class). Available options are "`T`": total numbers in each compartment as a function of time, "`A`": the default "agentinfo" output.

The `agentinfo` output has the following headers:

| AgentID | Farmer | ExposedOn | InfectedOn | RecoveredOn | Vaccinated | VaccinatedOn  | DaysExposed  | DaysInfected  | InfectingAgent   | InfectedAt  | NumberOfSecondaryInfections   | InfectionState |
|---------|--------|-----------|------------|-------------|------------|---------------|--------------|---------------|------------------|-------------|-------------------------------|----------------|

The above fields are returned for all non-susceptible agents. Values that are never set are negative (i.e. agents who are never vaccinated have a `VaccinatedOn` of -1000).

The `total` output has the following headers:

| Time | Susceptible | Exposed | Infected | Removed | BirdFOI | BirdS | BirdI | BirdR |
|------|-------------|---------|----------|---------|---------|--------|--------|--------|

The above fields show the aggregate number in each compartment, and the FOI details of the Bird SIR model as a function of time.


## License

[![CC BY-SA 4.0][cc-by-sa-shield]][cc-by-sa]

This work is licensed under a
[Creative Commons Attribution-ShareAlike 4.0 International License][cc-by-sa].

[![CC BY-SA 4.0][cc-by-sa-image]][cc-by-sa]

[cc-by-sa]: http://creativecommons.org/licenses/by-sa/4.0/
[cc-by-sa-image]: https://licensebuttons.net/l/by-sa/4.0/88x31.png
[cc-by-sa-shield]: https://img.shields.io/badge/License-CC%20BY--SA%204.0-lightgrey.svg
