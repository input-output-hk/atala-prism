import React from 'react';
import PropTypes from 'prop-types';
import { Badge } from 'antd';

const PhraseRenderer = ({ mnemonics }) =>
  mnemonics.map(({ value, index }) => (
    <Badge
      count={`${index + 1}. ${value}`}
      style={{ backgroundColor: '#FFF4F5', color: '#ff2d3b' }}
    />
  ));

PhraseRenderer.propTypes = {
  mnemonics: PropTypes.arrayOf(PropTypes.string)
};

export default PhraseRenderer;
