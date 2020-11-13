import React from 'react';
import PropTypes from 'prop-types';
import RowInfo from '../../atoms/row/Row';

import './_style.scss';

const DetailBox = ({ groups }) => (
  <div className="detailBox">
    <div className="infoRow">
      {groups.map(({ name, numberofcontacts }) => (
        <RowInfo key={name} contacts={numberofcontacts} groupName={name} />
      ))}
    </div>
  </div>
);

DetailBox.propTypes = {
  groups: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
      numberofcontacts: PropTypes.number
    })
  ).isRequired
};

export default DetailBox;
