import React from 'react';
import PropTypes from 'prop-types';
import OptionsHeader from '../Atoms/OptionsHeader';
import ContactCreationTable from '../Organisms/Tables/ContactCreationTable';
import { groupShape } from '../../../helpers/propShapes';
import './_style.scss';

const ManualContactCreation = ({ groupsProps, addEntity }) => (
  <div className="ManualImportWrapper ContactsImportWrapper">
    <div className="TitleContainer">
    <h3>Assign to a Group</h3>
    <p>Assign contacts to a single or multiple Group</p>
    </div>
    <div className="ContentHeader TitleAndSubtitle">
      <OptionsHeader groupsProps={groupsProps} addEntity={addEntity} />
    </div>
    <div className="ManualImportContent">
      <h3>Complete the contact information</h3>
      <ContactCreationTable />
    </div>
  </div>
);

ManualContactCreation.defaultProps = {
  addEntity: undefined
};

ManualContactCreation.propTypes = {
  groupsProps: PropTypes.shape({
    groups: PropTypes.arrayOf(groupShape).isRequired,
    selectedGroups: PropTypes.arrayOf(PropTypes.string).isRequired,
    setSelectedGroups: PropTypes.func.isRequired
  }).isRequired,
  addEntity: PropTypes.func
};

export default ManualContactCreation;
