{
  description = "BBMRI-ERIC Negotiator development tooling profile";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { nixpkgs, ... }:
    let
      lib = nixpkgs.lib;
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];
      forEachSystem = f:
        lib.genAttrs systems (system:
          let
            pkgs = import nixpkgs { inherit system; };
            toolPackages = [
              pkgs.jq
              pkgs.curl
              pkgs.git
              pkgs.gnumake
            ];
            devPackages =
              [
                pkgs.openjdk21
                pkgs.maven
                pkgs."jdt-language-server"
                pkgs.nodejs_24
                pkgs.yarn
              ]
              ++ toolPackages
              ++ lib.optionals pkgs.stdenv.isLinux [
                pkgs.docker
                pkgs."docker-compose"
              ];
            opencodePackages = devPackages ++ [
              pkgs.opencode
              pkgs.sqlite
            ];
            javaHome = "${pkgs.openjdk21}/lib/openjdk";
          in
          f pkgs {
            inherit devPackages opencodePackages javaHome;
          });
    in
    {
      packages = forEachSystem (pkgs: cfg: {
        dev = pkgs.buildEnv {
          name = "negotiator-dev-profile";
          paths = cfg.devPackages;
        };
        opencode = pkgs.buildEnv {
          name = "negotiator-opencode-profile";
          paths = cfg.opencodePackages;
        };
        default = pkgs.buildEnv {
          name = "negotiator-dev-profile";
          paths = cfg.devPackages;
        };
      });

      devShells = forEachSystem (pkgs: cfg: {
        dev = pkgs.mkShell {
          packages = cfg.devPackages;
          JAVA_HOME = cfg.javaHome;
          JDK_HOME = cfg.javaHome;
        };
        opencode = pkgs.mkShell {
          packages = cfg.opencodePackages;
          JAVA_HOME = cfg.javaHome;
          JDK_HOME = cfg.javaHome;
        };
        default = pkgs.mkShell {
          packages = cfg.devPackages;
          JAVA_HOME = cfg.javaHome;
          JDK_HOME = cfg.javaHome;
        };
      });
    };
}
