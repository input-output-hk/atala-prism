import React from 'react';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import contactLogo from '../../../../images/holder-default-avatar.svg';
import groupLogo from '../../../../images/groupIcon.svg';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { backendDateFormat } from '../../../../helpers/formatters';
import { useTranslationWithPrefix } from '../../../../hooks/useTranslationWithPrefix';
import { contactShape, groupShape } from '../../../../helpers/propShapes';

import './_style.scss';

const contactsPrefix = 'contacts.table.columns';
const groupsPrefix = 'groups.table.columns';

const RecipientsList = ({ recipients }) => {
  const tpc = useTranslationWithPrefix(contactsPrefix);
  const tpg = useTranslationWithPrefix(groupsPrefix);

  const recipientsWithKeys = recipients.map((r, index) => ({ ...r, key: index }));

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
            render: ({ contactName, name }) => (
              <CellRenderer
                title={contactName ? tpc('contactName') : tpg('groupName')}
                value={contactName || name}
              />
            )
          },
          {
            render: ({ externalId }) =>
              externalId && <CellRenderer title={tpc('externalId')} value={externalId} />
          },
          {
            render: ({ createdAt }) =>
              createdAt?.seconds && (
                <CellRenderer
                  title={tpc('creationDate')}
                  value={backendDateFormat(createdAt?.seconds)}
                />
              )
          }
        ]}
        dataSource={recipientsWithKeys}
      />
    </div>
  );
};

RecipientsList.propTypes = {
  recipients: PropTypes.arrayOf(PropTypes.oneOfType([groupShape, contactShape])).isRequired
};

export default RecipientsList;
