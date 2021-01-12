import React from 'react';
import PropTypes from 'prop-types';
import { Badge } from 'antd';

const PhraseRenderer = ({ mnemonics }) =>
  mnemonics.map(({ value, index }) => {
    const count = `${index + 1}. ${value}`;
    return (
      <Badge key={count} count={count} style={{ backgroundColor: '#FFF4F5', color: '#ff2d3b' }} />
    );
  });

PhraseRenderer.propTypes = {
  mnemonics: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string,
      index: PropTypes.number
    })
  )
};

export default PhraseRenderer;
