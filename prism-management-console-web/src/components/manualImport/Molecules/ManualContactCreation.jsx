import React from 'react';
import PropTypes from 'prop-types';
import OptionsHeader from '../Atoms/OptionsHeader';
import ContactCreationTable from '../Organisms/Tables/ContactCreationTable';

const ManualContactCreation = ({ groupsProps, addEntity }) => (
  <div className="ManualImportWrapper">
    <div className="ContentHeader TitleAndSubtitle">
      <OptionsHeader {...groupsProps} addEntity={addEntity} />
    </div>
    <div className="ManualImportContent">
      <ContactCreationTable />
    </div>
  </div>
);

ManualContactCreation.defaultProps = {
  addEntity: undefined
};

ManualContactCreation.propTypes = {
  groupsProps: PropTypes.shape({
    // TODO: add propTypes
  }).isRequired,
  addEntity: PropTypes.func
};

export default ManualContactCreation;
