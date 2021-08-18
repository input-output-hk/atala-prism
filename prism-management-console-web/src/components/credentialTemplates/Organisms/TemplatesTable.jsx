import React from 'react';
import PropTypes from 'prop-types';
import { getTemplatesColumns } from '../../../helpers/tableDefinitions/templates';
import InfiniteScrollTable from '../../common/Organisms/Tables/InfiniteScrollTable';
import { useCredentialTypes, useTemplateCategories } from '../../../hooks/useCredentialTypes';
import './_style.scss';

const TemplatesTable = ({
  // setGroupToDelete,
  // groups,
  // selectedGroups,
  // setSelectedGroups,
  // onCopy,
  // shouldSelectRecipients,
  // hasMore,
  // searching,
  // getMoreGroups
  credentialTypes,
  templateCategories
}) => {
  const tableProps = {
    columns: getTemplatesColumns(templateCategories),
    data: credentialTypes,
    rowKey: 'id'
  };

  return (
    <div className="TemplatesTableContainer">
      <InfiniteScrollTable {...tableProps} />;
    </div>
  );
};

TemplatesTable.defaultProps = {
  // TODO: Fill proptypes
};

TemplatesTable.propTypes = {
  // TODO: Fill proptypes
};

export default TemplatesTable;
