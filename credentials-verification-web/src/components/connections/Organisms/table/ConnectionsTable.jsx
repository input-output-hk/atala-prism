import React, { useState } from 'react';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import StatusBadge from '../../Atoms/StatusBadge/StatusBadge';
import { dayMonthYearBackendFormatter } from '../../../../helpers/formatters';
import {
  CONNECTION_STATUSES_TRANSLATOR,
  INDIVIDUAL_STATUSES_TRANSLATOR
} from '../../../../helpers/constants';
import ActionButtons from '../../Atoms/ActionButtons/ActionButtons';
import holderDefaultAvatar from '../../../../images/holder-default-avatar.svg';
import { subjectShape } from '../../../../helpers/propShapes';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';

import './_style.scss';

const STATUSES_TRANSLATOR = isIssuer =>
  isIssuer ? CONNECTION_STATUSES_TRANSLATOR : INDIVIDUAL_STATUSES_TRANSLATOR;

const getColumns = ({ inviteHolder, isIssuer, viewConnectionDetail }) => {
  const userColumn = [
    {
      key: 'avatar',
      width: 45,
      render: ({ avatar }) => (
        <img
          style={{ width: '40px', height: '40px' }}
          src={avatar || holderDefaultAvatar}
          alt="Avatar"
        />
      )
    },
    {
      key: 'contactName',
      render: ({ contactName }) => (
        <CellRenderer title="contactName" value={contactName} componentName="contacts" />
      )
    },
    {
      key: 'externalId',
      render: ({ externalId }) => (
        <CellRenderer title="externalId" value={externalId} componentName="contacts" />
      )
    }
  ];

  const issuerInfo = [
    {
      key: 'creationDate',
      render: ({ creationDate }) => (
        <CellRenderer
          title="creationDate"
          value={dayMonthYearBackendFormatter(creationDate)}
          componentName="contacts"
        />
      )
    }
  ];

  const genericColumns = [
    {
      key: 'credentials',
      render: ({ credentials }) => (
        <CellRenderer
          title="credentials"
          value={`(${credentials?.length || 0})`}
          componentName="contacts"
        />
      )
    },
    {
      key: 'connectionstatus',
      render: ({ status }) => {
        const statusDictionary = STATUSES_TRANSLATOR(isIssuer());
        const statusLabel = statusDictionary[status];

        return <StatusBadge status={statusLabel} />;
      }
    }
  ];

  const actionColumns = [
    {
      key: 'actions',
      render: holder => (
        <ActionButtons
          holder={holder}
          inviteHolder={inviteHolder}
          isIssuer={isIssuer}
          viewConnectionDetail={viewConnectionDetail}
        />
      )
    }
  ];

  const finalColumns = [];

  finalColumns.push(...userColumn);
  if (isIssuer()) finalColumns.push(...issuerInfo);
  finalColumns.push(...genericColumns);
  finalColumns.push(...actionColumns);

  return finalColumns;
};

const ConnectionsTable = ({
  subjects,
  setSelectedSubjects,
  selectedSubjects,
  inviteHolder,
  handleHoldersRequest,
  hasMore,
  isIssuer,
  viewConnectionDetail
}) => {
  const [loading, setLoading] = useState(false);

  const getMoreData = () => {
    setLoading(true);
    return handleHoldersRequest().finally(() => setLoading(false));
  };
  return (
    <div className="ConnectionsTable">
      <InfiniteScrollTable
        columns={getColumns({ inviteHolder, isIssuer, viewConnectionDetail })}
        data={subjects}
        loading={loading}
        getMoreData={getMoreData}
        hasMore={hasMore}
        rowKey="contactid"
        selectionType={
          setSelectedSubjects && {
            selectedRowKeys: selectedSubjects,
            type: 'checkbox',
            onChange: setSelectedSubjects
          }
        }
      />
    </div>
  );
};

ConnectionsTable.defaultProps = {
  subjects: [],
  setSelectedSubjects: null,
  selectedSubjects: []
};

ConnectionsTable.propTypes = {
  subjects: PropTypes.arrayOf(PropTypes.shape(subjectShape)),
  setSelectedSubjects: PropTypes.func,
  selectedSubjects: PropTypes.arrayOf(PropTypes.string),
  inviteHolder: PropTypes.func.isRequired,
  viewConnectionDetail: PropTypes.func.isRequired,
  isIssuer: PropTypes.func.isRequired,
  handleHoldersRequest: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired
};

export default ConnectionsTable;
