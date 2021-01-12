import React from 'react';
import PropTypes from 'prop-types';

import './_style.scss';

const Summary = ({
  type: {
    icon: { src, alt },
    type
  },
  info,
  amount
}) => (
  <div className="Summary">
    <div className="SummaryCard">
      <div className="CardType">
        <img src={src} alt={alt} />
        <p>{type}</p>
      </div>
      <p className="CardInfo">{info}</p>
    </div>
    <div className="SummaryValue">
      <h1>{amount}</h1>
    </div>
  </div>
);

Summary.propTypes = {
  type: PropTypes.shape({
    icon: PropTypes.shape({
      src: PropTypes.string,
      alt: PropTypes.string
    }),
    type: PropTypes.string
  }).isRequired,
  amount: PropTypes.number.isRequired,
  info: PropTypes.string.isRequired
};

export default Summary;
