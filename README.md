# Java 8 mod launcher for Minecraft
Intended as a replacement for LegacyLauncher, using Java 8 and 9 compatible constructs, in theory.

# NOTE
This is strictly for initial development and review of ideas. It will not be this github repo once complete, it will not be these packages once complete. *Please note this well - this is a holding area as I develop out the initial plans, a more permanent home has yet to be identified*


# TODO
- [ ] Everything
- [ ] Some things
- [ ] Make it work
- [X] Make it compile

In seriousness, areas that need to be reviewed: the details of voting contexts, environment contexts. The documentation on Voting should be OK, I think it reflects how I believe it should work.

### Things I want to have:
- a universal access transformer - access transformers are a bit of a special bunny, and everyone needs them all the time. Having it baked right into the basic framework removes a huge burden from a lot of places.
- logging. I want the logging to be very solid. If we die, I want a log file to tell us why immediately, no speculations.
- there is probably a third "initialization cycle" in the launcherservice that isn't represented yet - for once the classloader has stood up.
- we probably still need to do the signed but not signed jar hacks from legacylauncher.

### Tests
I want everything to have a test, if possible. I hate how unreproducible legacylauncher is. With Java9 ripping the rug out of how everything is *supposed* to work, having a broad test suite that represents "yup, it works" will be invaluable.

## On contributions
This code is often sitting on my desktop in a half working state as I take time to work on it. Time is on our side, 1.13 is the earliest this will land, and that is some time away yet. PRs, although welcome and invited, might be based on the repo as it existed some time before, and I will have taken the code in a new direction as I figure out things. At present, I'm not likely to accept a PR as a result. Once we get past the "initial conception" stages, where it is properly working and able to fulfill the role I'm giving it (soon, I hope), I anticipate seeking some PRs to implement enhancement features.

## On licensing
There's some complexity involved in this, because Mojang are interested in this themselves, and licensing may need to conform to their standards. At present, this code is therefore unlicensed and as such "all rights reserved". Contributions and contributors should take note of this. It's not ideal (LGPLv2.1 is my preferred target license for platform libs such as this, as you all know) but I do intend to address this before it goes live.
