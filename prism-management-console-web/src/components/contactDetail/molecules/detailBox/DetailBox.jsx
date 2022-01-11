import React from 'react';
import PropTypes from 'prop-types';
import RowInfo from '../../atoms/RowInfo/RowInfo';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';

import './_style.scss';

const DetailBox = ({ groups, contactId, loading, onDelete }) => (
  <div className="detailBox">
    {loading ? (
      <SimpleLoading />
    ) : (
      <div className="infoRow">
        {groups.map(({ id, name, numberOfContacts }) => (
          <RowInfo
            key={name}
            contacts={numberOfContacts}
            groupName={name}
            onDelete={onDelete}
            groupId={id}
            contactId={contactId}
          />
        ))}
      </div>
    )}
  </div>
);

DetailBox.defaultProps = {
  loading: false,
  onDelete: null,
  contactId: ''
};

DetailBox.propTypes = {
  groups: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
      numberofcontacts: PropTypes.number
    })
  ).isRequired,
  contactId: PropTypes.string,
  loading: PropTypes.bool,
  onDelete: PropTypes.func
};

export default DetailBox;
