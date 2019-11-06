import React from 'react';
import PropTypes from 'prop-types';
import { Badge } from 'antd';

const PhraseRenderer = ({ mnemonics }) =>
  mnemonics.map(({ value, index }) => <Badge count={`${index + 1}. ${value}`} />);

PhraseRenderer.propTypes = {
  mnemonics: PropTypes.arrayOf(PropTypes.string)
};

export default PhraseRenderer;
