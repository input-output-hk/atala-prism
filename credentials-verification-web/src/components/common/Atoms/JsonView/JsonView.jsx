import React from 'react';
import ReactJson from 'react-json-view';
import './_style.scss';

// keep comments for useful information
const theme = {
  base00: '#212121', // background
  base01: '#ddd',
  base02: '#ddd', // collapse line
  base03: '#444',
  base04: 'white',
  base05: 'white',
  base06: 'white',
  base07: '#3FE9E8', // keys (including ':' char)
  base08: '#3FE9E8',
  base09: '#ff2d3b', // values
  base0A: '#ff2d3b',
  base0B: '#ff2d3b',
  base0C: 'white',
  base0D: 'white', // collapse arrow
  base0E: 'white',
  base0F: 'white' // copy button
};

const JsonView = props => (
  <ReactJson
    theme={theme}
    displayDataTypes={false}
    displayObjectSize={false}
    collapseStringsAfterLength={20}
    name={null}
    {...props}
  />
);

export default JsonView;
