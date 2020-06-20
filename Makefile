SHELL=bash

.PHONY: clean start stop

clean: stop
	@docker container ls -q --filter status=exited --filter status=created | docker rm || true

start:
	@docker-compose up

stop:
	@docker-compose down
