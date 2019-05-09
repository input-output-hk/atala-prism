{ system ? builtins.currentSystem
, crossSystem ? null
, config ? {}
}:
import ./default.nix { inherit system crossSystem config; }