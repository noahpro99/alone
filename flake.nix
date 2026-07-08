{
  description = "Alone — a survival-realism overhaul modpack for Minecraft 26.2 (Fabric)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };

      # Minecraft 26.2 targets Java 25.
      jdk = pkgs.jdk25;

      # Native libraries that LWJGL / GLFW / OpenAL dlopen at runtime when
      # `./gradlew :core:runClient` launches the real game. On NixOS the actual
      # GPU driver lives at /run/opengl-driver/lib (prepended in the shellHook).
      runtimeLibs = with pkgs; [
        libGL
        glfw
        openal
        vulkan-loader        # 26.2 renders through Vulkan/Blaze3D
        libpulseaudio
        wayland
        libxkbcommon
        stdenv.cc.cc.lib     # libstdc++
        zlib
        libx11
        libxcursor
        libxrandr
        libxi
        libxxf86vm
        libxext
        libxrender
        libxtst
      ];
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = [
          jdk
          pkgs.gradle_9        # 9.5.1 — matches Loom 1.17; the default `gradle` is 8.14.4 and too old
          pkgs.prismlauncher   # playtesting the assembled modpack
          pkgs.git
        ];

        JAVA_HOME = "${jdk}";

        shellHook = ''
          export LD_LIBRARY_PATH="/run/opengl-driver/lib:${pkgs.lib.makeLibraryPath runtimeLibs}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
          echo "── Alone dev shell (Minecraft 26.2 / Fabric) ──"
          echo "  JDK:        $(java -version 2>&1 | head -n1)"
          echo "  First time: gradle wrapper --gradle-version 9.5.1"
          echo "  Dev client: ./gradlew :core:runClient"
          echo "  Playtest:   prismlauncher"
        '';
      };
    };
}
