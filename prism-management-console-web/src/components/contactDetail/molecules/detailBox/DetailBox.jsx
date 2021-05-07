import React from 'react';
import PropTypes from 'prop-types';
import RowInfo from '../../atoms/row/Row';

import './_style.scss';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';

const DetailBox = ({ groups, loading }) => (
  <div className="detailBox">
    {loading ? (
      <SimpleLoading />
    ) : (
      <div className="infoRow">
        {groups.map(({ name, numberOfContacts }) => (
          <RowInfo key={name} contacts={numberOfContacts} groupName={name} />
        ))}
      </div>
    )}
  </div>
);

DetailBox.defaultProps = {
  loading: false
};

DetailBox.propTypes = {
  groups: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
      numberofcontacts: PropTypes.number
    })
  ).isRequired,
  loading: PropTypes.bool
};

export default DetailBox;
