import React, { useState } from 'react';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import StatusBadge from '../../Atoms/StatusBadge/StatusBadge';
import { shortBackendDateFormatter } from '../../../../helpers/formatters';
import {
  CONNECTION_STATUSES,
  CONNECTION_STATUSES_TRANSLATOR,
  INDIVIDUAL_STATUSES,
  INDIVIDUAL_STATUSES_TRANSLATOR
} from '../../../../helpers/constants';
import ActionButtons from '../../Atoms/ActionButtons/ActionButtons';
import holderDefaultAvatar from '../../../../images/holder-default-avatar.svg';
import { infiniteTableProps, subjectShape } from '../../../../helpers/propShapes';
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
    { key: 'fullname', render: ({ fullname }) => fullname }
  ];

  const issuerInfo = [
    {
      key: 'admissionDate',
      render: ({ admissiondate }) => (
        <CellRenderer
          title="admissionDate"
          value={shortBackendDateFormatter(admissiondate)}
          componentName="connections"
        />
      )
    }
  ];

  const genericColumns = [
    {
      key: 'email',
      render: ({ email }) => (
        <CellRenderer title="email" value={email} componentName="connections" />
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

  const showQR = holder => {
    const invitationMissing = holder.status === CONNECTION_STATUSES.invitationMissing;

    const createdOrRevoked = [INDIVIDUAL_STATUSES.created, INDIVIDUAL_STATUSES.revoked].includes(
      holder.status
    );

    return invitationMissing || createdOrRevoked;
  };

  const actionColumns = [
    {
      key: 'actions',
      render: holder => (
        <ActionButtons
          holder={holder}
          showQRButton={showQR(holder)}
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
      />
    </div>
  );
};

ConnectionsTable.defaultProps = {
  subjects: []
};

ConnectionsTable.propTypes = {
  subjects: PropTypes.arrayOf(PropTypes.shape(subjectShape)),
  inviteHolder: PropTypes.func.isRequired,
  viewConnectionDetail: PropTypes.func.isRequired,
  isIssuer: PropTypes.func.isRequired,
  ...infiniteTableProps
};

export default ConnectionsTable;
