import React from 'react';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import contactLogo from '../../../../images/holder-default-avatar.svg';
import groupLogo from '../../../../images/groupIcon.svg';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { backendDateFormat } from '../../../../helpers/formatters';
import { useTranslationWithPrefix } from '../../../../hooks/useTranslationWithPrefix';

import './_style.scss';

const contactsPrefix = 'contacts.table.columns';
const groupsPrefix = 'groups.table.columns';

const RecipientsList = ({ recipients }) => {
  const tpc = useTranslationWithPrefix(contactsPrefix);
  const tpg = useTranslationWithPrefix(groupsPrefix);

  return (
    <div className="RecipientsList">
      <Table
        showHeader={false}
        pagination={false}
        columns={[
          {
            width: '60px',
            render: ({ contactId }) => (
              <img
                style={{ width: '40px', height: '40px' }}
                src={contactId ? contactLogo : groupLogo}
                alt="logo"
              />
            )
          },
          {
            render: ({ contactName, groupName }) => (
              <CellRenderer
                title={contactName ? tpc('contactName') : tpg('groupName')}
                value={contactName || groupName}
              />
            )
          },
          {
            render: ({ externalId }) =>
              externalId && <CellRenderer title={tpc('externalId')} value={externalId} />
          },
          {
            render: ({ createdat }) =>
              createdat?.seconds && (
                <CellRenderer
                  title={tpc('creationDate')}
                  value={backendDateFormat(createdat?.seconds)}
                />
              )
          }
        ]}
        dataSource={recipients}
      />
    </div>
  );
};

RecipientsList.propTypes = {
  recipients: PropTypes.arrayOf(
    PropTypes.oneOf([
      PropTypes.shape({ name: PropTypes.string }),
      PropTypes.shape({
        contactId: PropTypes.string,
        contactName: PropTypes.string,
        groupName: PropTypes.string,
        externalId: PropTypes.string,
        createdat: PropTypes.number
      })
    ])
  ).isRequired
};

export default RecipientsList;
