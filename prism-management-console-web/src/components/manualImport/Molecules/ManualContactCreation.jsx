import React from 'react';
import PropTypes from 'prop-types';
import OptionsHeader from '../Atoms/OptionsHeader';
import ContactCreationTable from '../Organisms/Tables/ContactCreationTable';

const ManualContactCreation = ({ tableProps, groupsProps, addEntity }) => (
  <div className="ManualImportWrapper">
    <div className="ContentHeader TitleAndSubtitle">
      <OptionsHeader {...groupsProps} addEntity={addEntity} />
    </div>
    <div className="ManualImportContent">
      <ContactCreationTable tableProps={tableProps} />
    </div>
  </div>
);

ManualContactCreation.propTypes = {
  // FIXME: add proptypes
};

export default ManualContactCreation;
