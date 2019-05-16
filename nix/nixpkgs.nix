# Imports nixpkgs.
# The version can be overridden for debugging purposes by setting
# NIX_PATH=custom_nixpkgs=/path/to/nixpkgs
let try = builtins.tryEval <custom_nixpkgs>;
in if try.success
then builtins.trace "using host <custom_nixpkgs>" try.value
else
  let
    spec = builtins.fromJSON (builtins.readFile ./nixpkgs-src.json);
  in builtins.fetchTarball {
    url = "${spec.url}/archive/${spec.rev}.tar.gz";
    inherit (spec) sha256;
    }
