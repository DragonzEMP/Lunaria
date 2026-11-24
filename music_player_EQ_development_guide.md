# Building a Best-in-Class EQ for an Android Music Player

## Goal

Build an equalizer that can compete with Poweramp by combining a
powerful audio-processing engine with a polished, intuitive interface.
The goal is not just to create sliders that look like an EQ: every
control must genuinely and reliably affect the audio.

Think of the EQ as a complete **audio processing engine (DSP)** with a
user interface on top.

------------------------------------------------------------------------

## 1. What a Top-Tier EQ Should Have

Divide the EQ into six major systems:

  -----------------------------------------------------------------------
  Feature                 What it does            Priority
  ----------------------- ----------------------- -----------------------
  Graphic EQ              Traditional frequency   Essential
                          sliders                 

  Parametric EQ           Precise control over    Essential
                          individual frequencies  

  Preamp                  Controls overall signal Essential
                          level                   

  Bass & Treble           Quick tonal adjustments Essential

  Stereo processing       Balance and stereo      Advanced
                          width                   

  Audio effects           Limiter, compressor,    Advanced
                          spatial effects         
  -----------------------------------------------------------------------

The most important principle is that the EQ must actually process audio
properly. A beautiful interface with sliders that produce poor or
distorted sound will not compete with Poweramp.

## 2. Build a 64-Band Parametric EQ

Use both a traditional graphic EQ and a more advanced parametric EQ.

### A. Graphic EQ

Provide several selectable band layouts:

-   5 bands
-   10 bands
-   15 bands
-   20 bands
-   31 bands
-   32 bands

Each band should have an adjustable gain, preferably from **-15 dB to
+15 dB**.

For the default 10-band EQ, use these center frequencies:

    Band   Frequency
  ------ -----------
       1       31 Hz
       2       62 Hz
       3      125 Hz
       4      250 Hz
       5      500 Hz
       6       1 kHz
       7       2 kHz
       8       4 kHz
       9       8 kHz
      10      16 kHz

The frequency distribution should be logarithmic, not linear.

### B. Parametric EQ

Allow users to create and configure individual filters instead of being
limited to fixed frequency sliders.

Each filter should expose:

  Parameter        Function
  ---------------- -----------------------------------------------
  Frequency        Selects the exact frequency
  Gain             Increases or decreases the frequency
  Q factor         Controls how wide or narrow the adjustment is
  Filter type      Determines how the frequency is affected
  Enable/Disable   Temporarily bypasses the filter

Support these filter types:

1.  Peaking / Bell
2.  Low Shelf
3.  High Shelf
4.  Low Pass
5.  High Pass
6.  Band Pass
7.  Notch

Example settings:

-   Filter 1: 120 Hz, +3 dB, Q = 1.0
-   Filter 2: 3,500 Hz, -2 dB, Q = 2.0

This gives users much more precise control than a traditional graphic
EQ.

### C. Interactive Frequency Response Graph

Include an interactive graph rather than relying only on sliders.

Requirements:

-   X-axis: 20 Hz to 20,000 Hz.
-   Y-axis: -15 dB to +15 dB.
-   Display the combined frequency-response curve.
-   Show individual draggable filter points.
-   Update the graph in real time as settings change.
-   Double-tap to add a filter.
-   Drag points to change frequency and gain.
-   Pinch to zoom.

The curve must represent the **combined response of all active
filters**, not just the currently selected filter.

## 3. Audio Processing Engine

This is probably the most important part of the project.

Build a dedicated DSP (Digital Signal Processing) engine rather than
relying entirely on Android's built-in equalizer.

### Suggested processing pipeline

``` text
Audio input
    |
    v
Decoded PCM audio
    |
    v
Preamp / Input gain
    |
    v
Graphic EQ / Parametric EQ
    |
    v
Tone & Bass processing
    |
    v
Stereo processing
    |
    v
Limiter / Output protection
    |
    v
Audio output
```

### Recommended technologies and techniques

-   **Biquad filters:** The foundation of the EQ.
-   **RBJ Audio EQ Cookbook:** A reference for calculating filter
    coefficients.
-   **32-bit floating-point processing:** Helps maintain headroom during
    processing.
-   **Real-time DSP:** EQ changes should happen without interrupting
    playback.
-   **Limiter:** Helps prevent excessive output peaks and clipping.

Important implementation details:

-   EQ changes should not create clicks, pops, or sudden volume jumps.
-   Use coefficient smoothing or crossfading when updating filters.
-   Avoid unnecessary memory allocations in the real-time audio
    callback.
-   Process left and right channels correctly.
-   Keep the DSP engine independent of the UI.
-   Test actual audio output; do not simulate processing by changing
    only the visual graph or slider positions.

Android also provides `DynamicsProcessing`, which includes EQ, multiband
compression, and limiter stages. Treat it as an optional platform
implementation rather than assuming it must be the entire audio engine.
The correct implementation depends on the app's playback architecture
and Android compatibility requirements.

## 4. Features That Can Make the EQ Stand Out

Build these after the core EQ works.

### A. AutoEQ / Headphone Profiles

Let users select their headphones or earbuds and apply an appropriate
correction profile. Eventually support importing AutoEQ profiles.

### B. Independent Bass & Treble

Provide dedicated bass and treble controls separate from the main EQ.

Suggested range: **-12 dB to +12 dB**.

### C. Stereo Expansion

Provide controls for:

-   Stereo width
-   Left/right balance
-   Mono conversion

Ensure stereo expansion does not introduce problematic phase effects.

### D. Compressor & Limiter

Provide dynamic-range control and protection against clipping.

Potential controls:

-   Threshold
-   Ratio
-   Attack
-   Release
-   Output ceiling

### E. Advanced Preset System

Allow users to:

-   Save unlimited custom presets.
-   Rename, duplicate, and delete presets.
-   Import and export settings.
-   Automatically load profiles for different audio devices, if
    supported by the app's architecture.

## 5. Presets: Make Them Actually Useful

Include a useful starting collection:

  Preset          Intended sound
  --------------- --------------------------------------
  Flat            Original audio / no tonal adjustment
  Bass Head       Stronger bass
  Deep Bass       Sub-bass emphasis
  Vocal Clarity   Clearer vocals
  Acoustic        Natural instruments
  Classical       Detailed orchestral sound
  Rock            Energetic instruments
  Electronic      Electronic music emphasis
  Podcast         Speech intelligibility
  Night Mode      Reduced dynamic range

Also implement **A/B Comparison**. Users should be able to switch
instantly between the original audio and their customized EQ without
losing their settings.

## 6. UI Design

Use a modern, dark, premium interface. Keep advanced features accessible
without overwhelming ordinary users.

### Suggested layout

**Top section** - EQ ON/OFF switch - Current preset - Reset button - A/B
comparison

**Middle section** - Interactive frequency-response graph - Graphic /
Parametric mode switch

**Bottom section** - EQ sliders or filter controls - Preamp - Bass and
Treble - Advanced settings

The UI should clearly distinguish between bypassed and active
processing. Show the current preset and make it easy to reset or compare
changes.

## 7. Development Roadmap

Do not ask the coding agent to build everything at once. Build and test
one phase at a time.

    Phase What to build
  ------- --------------------------------------------
        1 DSP engine and 10-band graphic EQ
        2 Parametric EQ and filter types
        3 Interactive frequency-response graph
        4 Preamp, bass and treble
        5 Limiter and compressor
        6 Presets and AutoEQ support
        7 Stereo processing
        8 Performance optimization and audio testing

**Architecture rule:** Keep the DSP engine independent of the UI. This
allows the interface to be redesigned without rewriting the
audio-processing engine.

Do not proceed to the next phase until the current phase is implemented
and tested.

------------------------------------------------------------------------

## 8. Initial Development Prompt for Antigravity

Use this as the foundation for the EQ project. It deliberately focuses
on architecture and the first development phase.

> I want to build a professional-grade audio equalizer for my Android
> music player, inspired by Poweramp. Do not build the entire system at
> once.
>
> First inspect my existing project and identify its programming
> language, framework, audio playback implementation, and architecture.
> Preserve existing functionality.
>
> My long-term goal is a premium DSP engine featuring:
>
> 1.  Configurable graphic EQ (5, 10, 15, 20, 31 and 32 bands).
> 2.  Parametric EQ with up to 64 independently configurable bands.
> 3.  Peaking, low shelf, high shelf, low pass, high pass, band pass and
>     notch filters.
> 4.  Interactive frequency-response graph.
> 5.  Preamp, independent bass and treble controls.
> 6.  Compressor, limiter and stereo processing.
> 7.  Preset management, import/export and AutoEQ headphone profiles.
>
> Technical requirements:
>
> -   Build an independent DSP engine rather than coupling audio
>     processing to UI components.
> -   Use accurate biquad filter implementations based on the RBJ Audio
>     EQ Cookbook.
> -   Use 32-bit floating-point processing where supported.
> -   Use logarithmic frequency distribution.
> -   Implement smooth real-time filter updates without clicks or pops.
> -   Maintain sufficient headroom and prevent digital clipping with an
>     output limiter.
> -   Process stereo channels correctly.
> -   Avoid unnecessary allocations in the real-time audio callback.
> -   Persist EQ settings and presets.
> -   Include unit tests for filter coefficients, frequency response,
>     gain, clipping and channel handling.
> -   Do not fake audio processing with UI-only sliders or visual
>     curves.
>
> **FIRST TASK ONLY:** Inspect the existing project and implement Phase
> 1: a functional DSP engine and a 10-band graphic equalizer. Explain
> the proposed architecture and files before making major changes.
> Verify that changing each slider actually changes the sound. Do not
> proceed to Phase 2 until Phase 1 is tested and working.

------------------------------------------------------------------------

## 9. Information Needed Before Implementation

The exact implementation depends on the music player's technology and
audio playback engine.

### Question 1: What technology is being used?

-   Native Android (Kotlin/Java)
-   Flutter (Dart)
-   React Native
-   Web technology (HTML/CSS/JavaScript)
-   Other / Not sure

### Question 2: Which audio playback engine is being used?

-   ExoPlayer / Media3
-   Android MediaPlayer
-   Custom audio engine
-   Not decided / Not sure

Once these are known, the DSP architecture and implementation plan can
be tailored to the actual project.

------------------------------------------------------------------------

## Final Goal

The ultimate goal is an EQ where someone who has used Poweramp can
switch to this music player and not feel that they are losing audio
customization capabilities. Ideally, the player should give them even
more control.

The priorities are:

1.  Accurate audio processing.
2.  Reliable, artifact-free real-time adjustments.
3.  Powerful graphic and parametric EQ.
4.  A clear, responsive frequency-response graph.
5.  Useful presets and headphone profiles.
6.  A polished, intuitive interface.

Build the audio engine first, verify that it works, and then expand the
feature set.
