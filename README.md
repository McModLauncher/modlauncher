# Java 8 mod launcher for Minecraft
Intended as a replacement for LegacyLauncher, using Java 8 and 9 compatible constructs, in theory.

# NOTE
This is strictly for initial development and review of ideas. It will not be this github repo once complete, it will not be these packages once complete.


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
