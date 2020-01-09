import React from 'react';
import PropTypes from 'prop-types';

import './_style.scss';

const IntroItem = ({ itemIcon, itemTitle, itemText }) => (
  <div className="IntroItem">
    <img src={itemIcon} alt="Item Icon" />
    <h2>{itemTitle}</h2>
    <p>{itemText}</p>
  </div>
);

IntroItem.propTypes = {
  itemIcon: PropTypes.string.isRequired,
  itemTitle: PropTypes.string.isRequired,
  itemText: PropTypes.string.isRequired
};

export default IntroItem;
