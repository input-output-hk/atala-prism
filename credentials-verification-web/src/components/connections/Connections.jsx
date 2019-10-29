import React, { Fragment, useState } from 'react';
import PropTypes from 'prop-types';
import { Drawer, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import { connectionShape } from '../../helpers/propShapes';
import DeletionModal from '../common/Organisms/Modals/DeletionModal/DeletionModal';
import ConnectionFilters from './Molecules/Filters/ConnectionFilters';
import ConnectionTable from './Organisms/Tables/ConnectionTable';
import ConnectionDetail from './Organisms/Detail/ConnectionDetail';
import noGroups from '../../images/noGroups.svg';

const Connections = ({
  connections,
  count,
  offset,
  setOffset,
  setDate,
  setName,
  handleConnectionDeletion
}) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [currentConnection, setCurrentConnection] = useState({});
  const [showDrawer, setShowDrawer] = useState();

  const closeModal = () => {
    setOpen(false);
    setCurrentConnection({});
  };

  const modalProps = {
    toDelete: { name: currentConnection.name, id: currentConnection.id },
    open,
    closeModal,
    handleDeletion: handleConnectionDeletion,
    prefix: 'connections'
  };

  return (
    <Fragment>
      <Drawer
        title={t('connections.detail.title')}
        placement="right"
        onClose={() => setShowDrawer(false)}
        visible={showDrawer}
      >
        <ConnectionDetail {...currentConnection} />
      </Drawer>
      <DeletionModal {...modalProps} />
      <div className="ContentHeader">
        <h1>{t('connections.title')}</h1>
      </div>
      <ConnectionFilters changeDate={setDate} changeFilter={setName} />
      <Row>
        {connections.length ? (
          <ConnectionTable
            setOpen={setOpen}
            setCurrentConnection={setCurrentConnection}
            connections={connections}
            current={offset + 1}
            total={count}
            offset={offset}
            onPageChange={value => setOffset(value - 1)}
            openDrawer={() => setShowDrawer(true)}
          />
        ) : (
          <EmptyComponent
            photoSrc={noGroups}
            photoAlt="connections.noConnections.photoAlt"
            title="connections.noConnections.title"
          />
        )}
      </Row>
    </Fragment>
  );
};

Connections.defaultProps = {
  connections: [],
  count: 0,
  offset: 0
};

Connections.propTypes = {
  connections: PropTypes.arrayOf(connectionShape),
  count: PropTypes.number,
  offset: PropTypes.number,
  setOffset: PropTypes.func.isRequired,
  setDate: PropTypes.func.isRequired,
  setName: PropTypes.func.isRequired,
  handleConnectionDeletion: PropTypes.func.isRequired
};

export default Connections;
