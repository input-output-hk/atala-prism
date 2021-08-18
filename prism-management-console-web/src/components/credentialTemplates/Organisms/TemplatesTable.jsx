import React from 'react';
import PropTypes from 'prop-types';
import { getTemplatesColumns } from '../../../helpers/tableDefinitions/templates';
import InfiniteScrollTable from '../../common/Organisms/Tables/InfiniteScrollTable';
import { credentialTypeShape, templateCategoryShape } from '../../../helpers/propShapes';
import './_style.scss';

const TemplatesTable = ({ credentialTypes, templateCategories, showTemplatePreview }) => {
  const tableActions = {
    showTemplatePreview
  };

  const tableProps = {
    columns: getTemplatesColumns(templateCategories, tableActions),
    data: credentialTypes,
    rowKey: 'id'
  };

  return (
    <div className="TemplatesTableContainer">
      <InfiniteScrollTable {...tableProps} />;
    </div>
  );
};

TemplatesTable.propTypes = {
  credentialTypes: PropTypes.arrayOf(credentialTypeShape).isRequired,
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
  showTemplatePreview: PropTypes.func.isRequired
};

export default TemplatesTable;
